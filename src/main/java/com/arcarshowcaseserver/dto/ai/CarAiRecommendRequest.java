package com.arcarshowcaseserver.dto.ai;

public class CarAiRecommendRequest {

    private String userNeed = "";
    private String dataSource = "AUTO";
    private Integer maxCars = 6;

    public String getUserNeed() {
        return userNeed;
    }

    public void setUserNeed(String userNeed) {
        this.userNeed = userNeed;
    }

    public String getDataSource() {
        return dataSource;
    }

    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }

    public Integer getMaxCars() {
        return maxCars;
    }

    public void setMaxCars(Integer maxCars) {
        this.maxCars = maxCars;
    }
}
