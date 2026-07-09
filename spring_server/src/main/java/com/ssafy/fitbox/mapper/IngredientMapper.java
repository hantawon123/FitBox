package com.ssafy.fitbox.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.ssafy.fitbox.dto.Ingredient;

@Mapper
public interface IngredientMapper {
    List<Ingredient> selectAll();
    Ingredient selectById(int id);
    int insert(Ingredient ingredient);
    int update(Ingredient ingredient);
    int delete(int id);
}