package com.woniuxy.service.impl.DocToPptImpl.DTO;

import lombok.Data;
import java.util.List;

@Data
public class PptTotalDTO {
    private String mainTitle;
    private String mainColor;
    private String themeStyle;
    private List<PptPageDTO> pages;
    private String pptUrl;
}