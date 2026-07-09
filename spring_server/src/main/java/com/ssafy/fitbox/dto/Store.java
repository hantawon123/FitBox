package com.ssafy.fitbox.dto;

public class Store {

    private Long id;
    private String name;
    private String address;
    private Double longitude;
    private Double latitude;

    // 날짜별 픽업 가능 수량
    // store_table 컬럼이 아니라 store_daily_capacity_table에서 JOIN해서 가져오는 값
    private Integer totalCnt;
    private Integer remainCnt;

    public Store() {
    }

    public Store(
            Long id,
            String name,
            String address,
            Double longitude,
            Double latitude,
            Integer totalCnt,
            Integer remainCnt
    ) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.longitude = longitude;
        this.latitude = latitude;
        this.totalCnt = totalCnt;
        this.remainCnt = remainCnt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Integer getTotalCnt() {
        return totalCnt;
    }

    public void setTotalCnt(Integer totalCnt) {
        this.totalCnt = totalCnt;
    }

    public Integer getRemainCnt() {
        return remainCnt;
    }

    public void setRemainCnt(Integer remainCnt) {
        this.remainCnt = remainCnt;
    }
}