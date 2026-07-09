package com.ssafy.fitbox.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.ssafy.fitbox.dto.Address;

@Mapper
public interface AddressMapper {
    List<Address> selectAll();
    Address selectById(int id);
    List<Address> selectByUserId(Integer userId);
    int insert(Address address);
    int update(Address address);
    int delete(int id);
}
