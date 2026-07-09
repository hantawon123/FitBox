package com.ssafy.fitbox.service;

import java.util.List;

import com.ssafy.fitbox.dto.Address;

public interface AddressService {

    List<Address> getAddresses();

    Address getAddress(int id);

    List<Address> getAddressesByUserId(Integer userId);

    boolean createAddress(Address address);

    boolean updateAddress(Address address);

    boolean deleteAddress(int id);
}
