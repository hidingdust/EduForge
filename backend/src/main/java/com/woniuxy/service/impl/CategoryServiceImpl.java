package com.woniuxy.service.impl;

import com.woniuxy.entity.Category;
import com.woniuxy.entity.VO.CategoryTreeVO;
import com.woniuxy.repository.CategoryRepository;
import com.woniuxy.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    @Override
    public List<Category> getRootList() {
        return categoryRepository.findByParentId(0);
    }

    @Override
    public List<Category> getChildByParentId(Integer parentId) {
        return categoryRepository.findByParentId(parentId);
    }

    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    public Optional<Category> findFirstByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        List<Category> list = categoryRepository.findByNameAndDeletedFalse(name.trim());
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public Optional<Category> findChildByParentIdAndName(Integer parentId, String name) {
        if (parentId == null || name == null || name.isBlank()) {
            return Optional.empty();
        }
        List<Category> list = categoryRepository.findByParentIdAndNameAndDeletedFalse(parentId, name.trim());
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public List<CategoryTreeVO> buildCategoryTree() {
        List<Category> roots = getRootList();
        return roots.stream()
                .filter(r -> r != null && !Boolean.TRUE.equals(r.getDeleted()))
                .map(root -> {
                    CategoryTreeVO rootVo = new CategoryTreeVO();
                    rootVo.setId(root.getId());
                    rootVo.setName(root.getName());

                    List<CategoryTreeVO> children = getChildByParentId(root.getId()).stream()
                            .filter(c -> c != null && !Boolean.TRUE.equals(c.getDeleted()))
                            .map(child -> {
                                CategoryTreeVO childVo = new CategoryTreeVO();
                                childVo.setId(child.getId());
                                childVo.setName(child.getName());
                                return childVo;
                            })
                            .collect(Collectors.toList());
                    rootVo.setChildren(children);
                    return rootVo;
                })
                .collect(Collectors.toList());
    }

}
