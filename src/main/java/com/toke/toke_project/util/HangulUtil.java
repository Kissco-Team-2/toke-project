package com.toke.toke_project.util;

public class HangulUtil {
    private static final char HANGUL_BASE = 0xAC00;
    private static final char HANGUL_LAST = 0xD7A3;
    private static final char[] GROUP14 = {
        '가','가','나','다','다','라','마','바','바','사','사','아','자','자','차','카','타','파','하'
    };

    public static String groupOf(String s){
        if (s==null || s.isBlank()) return null;
        char c = s.trim().charAt(0);
        if (c < HANGUL_BASE || c > HANGUL_LAST) return "#";
        int idx = (c - HANGUL_BASE) / (21*28);
        return String.valueOf(GROUP14[idx]);
    }

    public static int vowelIndexOf(String s){
        if (s==null || s.isBlank()) return 99;
        char c = s.trim().charAt(0);
        if (c < HANGUL_BASE || c > HANGUL_LAST) return 99;
        int syllable = c - HANGUL_BASE;
        return (syllable % (21*28)) / 28; // 중성 index 0~20
    }
}
