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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.fitbox.dto.Meal;
import com.ssafy.fitbox.service.MealService;

@RestController
@CrossOrigin("*")
@RequestMapping("/mealapi")
public class MealRestController {

    @Autowired
    MealService mealService;

    @GetMapping("/meal")
    public ResponseEntity<?> selectAll() {
        ArrayList<Meal> list = mealService.selectAll();
        if (list == null || list.isEmpty()) {
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<ArrayList<Meal>>(list, HttpStatus.OK);
    }

    @GetMapping("/meal/{id}")
    public ResponseEntity<?> selectById(@PathVariable("id") Long id) {
        Meal meal = mealService.selectById(id);
        if (meal == null) {
            return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
        }
        return new ResponseEntity<Meal>(meal, HttpStatus.OK);
    }

    @PostMapping("/meal")
    public ResponseEntity<?> insert(@RequestBody Meal meal) {
        int result = mealService.insert(meal);
        if (result > 0) {
            return new ResponseEntity<Meal>(meal, HttpStatus.CREATED);
        }
        return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @DeleteMapping("/meal/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        int result = mealService.delete(id);
        if (result > 0) {
            return new ResponseEntity<String>("success", HttpStatus.OK);
        }
        return new ResponseEntity<String>("fail", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}