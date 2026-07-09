package com.ssafy.fitbox.controller;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.fitbox.dto.PickupPoint;
import com.ssafy.fitbox.dto.Store;
import com.ssafy.fitbox.service.StoreService;

@RestController
@CrossOrigin("*")
@RequestMapping("/storeapi")
public class StoreRestController {

    @Autowired
    StoreService storeService;

    @GetMapping("/store")
    public ResponseEntity<?> selectAll(
            @RequestParam(value = "pickupDate", required = false) String pickupDate
    ) {
        ArrayList<Store> list;

        if (pickupDate == null || pickupDate.isBlank()) {
            list = storeService.selectAll();
        } else {
            list = storeService.findStoresByPickupDate(pickupDate);
        }

        if (list == null || list.isEmpty()) {
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<ArrayList<Store>>(list, HttpStatus.OK);
    }

    @GetMapping("/store/{id}")
    public ResponseEntity<?> selectById(@PathVariable("id") Long id) {
        Store store = storeService.selectById(id);

        if (store == null) {
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<Store>(store, HttpStatus.OK);
    }

    @PostMapping("/store")
    public ResponseEntity<?> insert(@RequestBody Store store) {
        int result = storeService.insert(store);

        if (result > 0) {
            return new ResponseEntity<Store>(store, HttpStatus.CREATED);
        }

        return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @PutMapping("/store/{id}")
    public ResponseEntity<?> update(
            @PathVariable("id") Long id,
            @RequestBody Store store
    ) {
        store.setId(id);

        int result = storeService.update(store);

        if (result > 0) {
            return new ResponseEntity<String>("success", HttpStatus.OK);
        }

        return new ResponseEntity<String>("fail", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @DeleteMapping("/store/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        int result = storeService.delete(id);

        if (result > 0) {
            return new ResponseEntity<String>("success", HttpStatus.OK);
        }

        return new ResponseEntity<String>("fail", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @GetMapping("/store/subscription")
    public ResponseEntity<?> selectStoresForSubscription(
            @RequestParam String dateStart,
            @RequestParam String dateEnd,
            @RequestParam(defaultValue = "false") Boolean mon,
            @RequestParam(defaultValue = "false") Boolean tue,
            @RequestParam(defaultValue = "false") Boolean wed,
            @RequestParam(defaultValue = "false") Boolean thu,
            @RequestParam(defaultValue = "false") Boolean fri,
            @RequestParam(defaultValue = "false") Boolean sat,
            @RequestParam(defaultValue = "false") Boolean sun
    ) {
        ArrayList<Store> list = storeService.findStoresBySubscriptionCondition(
                dateStart,
                dateEnd,
                mon,
                tue,
                wed,
                thu,
                fri,
                sat,
                sun
        );

        if (list == null || list.isEmpty()) {
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<ArrayList<Store>>(list, HttpStatus.OK);
    }

    @GetMapping("/pickup-points")
    public ResponseEntity<?> selectPickupPoints(
            @RequestParam Long storeId,
            @RequestParam(value = "pickupDate", required = false) String pickupDate
    ) {
        ArrayList<PickupPoint> list = storeService.findPickupPointsByStoreId(storeId, pickupDate);

        if (list == null || list.isEmpty()) {
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<ArrayList<PickupPoint>>(list, HttpStatus.OK);
    }

    @GetMapping("/pickup-points/bounds")
    public ResponseEntity<?> selectPickupPointsByBounds(
            @RequestParam Double south,
            @RequestParam Double north,
            @RequestParam Double west,
            @RequestParam Double east,
            @RequestParam(value = "pickupDate", required = false) String pickupDate
    ) {
        ArrayList<PickupPoint> list = storeService.findPickupPointsByBounds(
                south,
                north,
                west,
                east,
                pickupDate
        );

        if (list == null || list.isEmpty()) {
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<ArrayList<PickupPoint>>(list, HttpStatus.OK);
    }

    @GetMapping("/pickup-points/{id}/capacity")
    public ResponseEntity<?> selectPickupPointCapacity(
            @PathVariable("id") Long id,
            @RequestParam String pickupDate
    ) {
        PickupPoint point = storeService.findPickupPointCapacity(id, pickupDate);

        if (point == null) {
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<PickupPoint>(point, HttpStatus.OK);
    }
}
