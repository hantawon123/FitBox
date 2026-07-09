package com.ssafy.fitbox.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ssafy.fitbox.dto.Address;
import com.ssafy.fitbox.mapper.AddressMapper;

@Service
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    public AddressServiceImpl(AddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    @Override
    public List<Address> getAddresses() {
        return addressMapper.selectAll();
    }

    @Override
    public Address getAddress(int id) {
        return addressMapper.selectById(id);
    }

    @Override
    public List<Address> getAddressesByUserId(Integer userId) {
        return addressMapper.selectByUserId(userId);
    }

    @Override
    public boolean createAddress(Address address) {
        if (address == null
                || address.getUserId() == null
                || ((address.getRoadAddress() == null || address.getRoadAddress().isBlank())
                    && (address.getAddress() == null || address.getAddress().isBlank()))) {
            return false;
        }
        address.normalizeAddressFields();
        return addressMapper.insert(address) == 1;
    }

    @Override
    public boolean updateAddress(Address address) {
        if (address != null) {
            address.normalizeAddressFields();
        }
        return addressMapper.update(address) == 1;
    }

    @Override
    public boolean deleteAddress(int id) {
        return addressMapper.delete(id) == 1;
    }
}
