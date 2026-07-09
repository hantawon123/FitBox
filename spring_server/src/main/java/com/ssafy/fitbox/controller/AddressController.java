package com.ssafy.fitbox.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ssafy.fitbox.dto.Address;
import com.ssafy.fitbox.service.AddressService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Address API", description = "사용자 주소 CRUD API")
@RestController
@CrossOrigin("*")
@RequestMapping("/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @Operation(summary = "주소 전체 조회", description = "address_table에 저장된 모든 주소 정보를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<Address>> getAddresses() {
        return ResponseEntity.ok(addressService.getAddresses());
    }

    @Operation(summary = "주소 단건 조회", description = "기본키 id를 기준으로 주소 하나를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<Address> getAddress(
            @Parameter(description = "주소 기본키 id", example = "1")
            @PathVariable int id
    ) {
        Address address = addressService.getAddress(id);

        if (address == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(address);
    }

    @Operation(summary = "사용자별 주소 조회", description = "user_id를 기준으로 해당 사용자의 주소 목록을 조회합니다.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Address>> getAddressByUserId(
            @Parameter(description = "사용자 기본키", example = "1")
            @PathVariable Integer userId
    ) {
        return ResponseEntity.ok(addressService.getAddressesByUserId(userId));
    }

    @Operation(summary = "주소 등록", description = "새로운 주소를 address_table에 등록합니다.")
    @PostMapping
    public ResponseEntity<?> createAddress(@RequestBody Address address) {
        boolean result = addressService.createAddress(address);

        if (result) {
            return ResponseEntity.ok(address);
        }

        return ResponseEntity.badRequest().body("주소 등록 실패");
    }

    @Operation(summary = "주소 수정", description = "기본키 id를 기준으로 주소 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<String> updateAddress(
            @Parameter(description = "수정할 주소 기본키 id", example = "1")
            @PathVariable int id,
            @RequestBody Address address
    ) {
        address.setId(id);

        boolean result = addressService.updateAddress(address);

        if (result) {
            return ResponseEntity.ok("주소 수정 성공");
        }

        return ResponseEntity.badRequest().body("주소 수정 실패");
    }

    @Operation(summary = "주소 삭제", description = "기본키 id를 기준으로 주소 정보를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAddress(
            @Parameter(description = "삭제할 주소 기본키 id", example = "1")
            @PathVariable int id
    ) {
        boolean result = addressService.deleteAddress(id);

        if (result) {
            return ResponseEntity.ok("주소 삭제 성공");
        }

        return ResponseEntity.badRequest().body("주소 삭제 실패");
    }
}
