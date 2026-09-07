package com.woniuxy.controller;

import com.woniuxy.entity.Category;
import com.woniuxy.entity.VO.CategoryTreeVO;
import com.woniuxy.service.CategoryService;
import com.woniuxy.utils.ResponseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    //获取一级分类
    @PostMapping("/getRootList")
    public ResponseResult getRootList() {
        List<Category> rootList = categoryService.getRootList();
        return ResponseResult.success(rootList);
    }

    @PostMapping("/getChild")
    public ResponseResult getChild(@RequestParam Integer parentId){
        List<Category> childList=categoryService.getChildByParentId(parentId);
        return ResponseResult.success(childList);
    }

    @PostMapping("/getAll")
    public ResponseResult getAllList() {
        List<Category> AllList=categoryService.findAll();
        return ResponseResult.success(AllList);
    }

    /**
     * 供前端上传页使用的两级分类级联树
     */
    @GetMapping("/cascade")
    public ResponseResult cascade() {
        List<CategoryTreeVO> tree = categoryService.buildCategoryTree();
        return ResponseResult.success(tree);
    }
}
