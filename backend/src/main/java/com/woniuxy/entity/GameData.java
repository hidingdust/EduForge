package com.woniuxy.entity;

import lombok.Data;

import java.util.List;

@Data
public class GameData {
    private List<Question> questions;

    @Data
    public static class Question {
        private String question;
        private String A;
        private String B;
        private String C;
        private String D;
        private String answer;
    }
}