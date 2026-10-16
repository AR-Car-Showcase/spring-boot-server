package com.arcarshowcaseserver.service;

import com.arcarshowcaseserver.dto.CarDTO;

import java.util.List;

public interface LikeService {

    void likeCar(Long carId);

    void unlikeCar(Long carId);

    List<CarDTO> getUserLikedCars();

    Boolean hasUserLikedCar(Long carId);
}
