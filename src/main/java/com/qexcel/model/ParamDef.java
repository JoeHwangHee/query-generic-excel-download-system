package com.qexcel.model;

/**
 * 쿼리 내 '?' 하나에 대응하는 파라미터 정의(메타데이터).
 */
public class ParamDef {

    /** 쿼리 내 '?' 순서 (1부터) */
    private int seq;
    /** 입력 데이터 형식 */
    private ParamType type = ParamType.TEXT;
    /** type 이 DATE 일 때의 날짜 포맷 (그 외엔 null) */
    private DateFormatType dateFormat;
    /** 화면 입력칸에 표시할 라벨 (선택) */
    private String label;

    public ParamDef() {
    }

    public ParamDef(int seq, ParamType type, DateFormatType dateFormat, String label) {
        this.seq = seq;
        this.type = type;
        this.dateFormat = dateFormat;
        this.label = label;
    }

    public int getSeq() {
        return seq;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    public ParamType getType() {
        return type;
    }

    public void setType(ParamType type) {
        this.type = type;
    }

    public DateFormatType getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(DateFormatType dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
