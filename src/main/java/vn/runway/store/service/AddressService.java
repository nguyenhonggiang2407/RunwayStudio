package vn.runway.store.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.runway.store.dto.*;
import vn.runway.store.model.CustomerAddress;
import vn.runway.store.repository.*;
import vn.runway.store.web.ApiErrors.*;

@Service
@Transactional(readOnly = true)
public class AddressService {
  private final CustomerAddressRepository addresses;
  private final AccountRepository accounts;
  private final CurrentAccount current;

  public AddressService(CustomerAddressRepository a, AccountRepository r, CurrentAccount c) {
    addresses = a;
    accounts = r;
    current = c;
  }

  private Responses.Address dto(CustomerAddress a) {
    return new Responses.Address(a.id, a.recipient, a.phone, a.address);
  }

  public List<Responses.Address> list() {
    return addresses.findByAccountIdOrderByIdDesc(current.require().id).stream()
        .map(this::dto)
        .toList();
  }

  @Transactional
  public Responses.Address save(Long id, Requests.Address i) {
    Long actor = current.require().id;
    accounts.lockById(actor).orElseThrow();
    var a =
        id == null
            ? new CustomerAddress()
            : addresses
                .findById(id)
                .filter(x -> x.accountId.equals(actor))
                .orElseThrow(() -> new Missing("Không tìm thấy địa chỉ."));
    if (id == null && addresses.countByAccountId(actor) >= 10)
      throw new BadRequest("Tối đa 10 địa chỉ.");
    a.accountId = actor;
    a.recipient = i.recipient().strip();
    a.phone = i.phone();
    a.address = i.address().strip();
    addresses.saveAndFlush(a);
    return dto(a);
  }

  @Transactional
  public void delete(Long id) {
    Long actor = current.require().id;
    accounts.lockById(actor).orElseThrow();
    addresses.delete(
        addresses
            .findById(id)
            .filter(a -> a.accountId.equals(actor))
            .orElseThrow(() -> new Missing("Không tìm thấy địa chỉ.")));
  }
}
