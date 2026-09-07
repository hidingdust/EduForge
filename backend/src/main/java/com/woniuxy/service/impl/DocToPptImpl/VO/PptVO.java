package com.woniuxy.service.impl.DocToPptImpl.VO;

import com.woniuxy.service.impl.DocToPptImpl.DTO.PptPageDTO;
import lombok.Data;
import java.util.List;

@Data
public class PptVO {
    private String mainTitle;
    private String mainColor;
    private String themeStyle;
    private List<PptPageDTO> pages;
    private String pptUrl;
}