package com.arcarshowcaseserver.dto.ai;

import java.util.ArrayList;
import java.util.List;

public class CarAiCompareRequest {

    private List<Long> carIds = new ArrayList<>();
    private List<String> carNames = new ArrayList<>();
    private String userNeed = "";
    private String dataSource = "AUTO";

    public List<Long> getCarIds() {
        return carIds;
    }

    public void setCarIds(List<Long> carIds) {
        this.carIds = carIds;
    }

    public List<String> getCarNames() {
        return carNames;
    }

    public void setCarNames(List<String> carNames) {
        this.carNames = carNames;
    }

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
}
