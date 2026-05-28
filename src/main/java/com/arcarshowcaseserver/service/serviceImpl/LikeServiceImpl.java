package com.arcarshowcaseserver.service.serviceImpl;

import java.time.LocalDateTime;

import com.arcarshowcaseserver.dto.CarDTO;
import com.arcarshowcaseserver.exceptions.DuplicateLikeException;
import com.arcarshowcaseserver.exceptions.ResourceNotFoundException;
import com.arcarshowcaseserver.model.Cars.Car;
import com.arcarshowcaseserver.model.Cars.Like;
import com.arcarshowcaseserver.model.User;
import com.arcarshowcaseserver.repository.CarRepository;
import com.arcarshowcaseserver.repository.LikeRepository;
import com.arcarshowcaseserver.repository.UserRepository;
import com.arcarshowcaseserver.security.CurrentAuthenticatedUserService;
import com.arcarshowcaseserver.service.LikeService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class LikeServiceImpl implements LikeService {

    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    private final CarRepository carRepository;
    private final CurrentAuthenticatedUserService currentAuthenticatedUserService;

    public LikeServiceImpl(UserRepository userRepository,
                           LikeRepository likeRepository,
                           CarRepository carRepository,
                           CurrentAuthenticatedUserService currentAuthenticatedUserService) {
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
        this.carRepository = carRepository;
        this.currentAuthenticatedUserService = currentAuthenticatedUserService;
    }


    @Override
    public void likeCar(Long carId) {
        User user = retriveLoggedInUser();
        Car car = carRepository.findById(carId).orElseThrow(
                ()-> new ResourceNotFoundException("Car not found with id: " + carId)
        );
        if (likeRepository.existsByCarIdAndUserId(carId, user.getId())) {
            throw new DuplicateLikeException("Car already liked");
        }

        Like like = new Like();
        like.setCar(car);
        like.setUser(user);
        like.setLikedAt(LocalDateTime.now());
        likeRepository.save(like);
    }

    @Override
    public void unlikeCar(Long carId) {
        User user = retriveLoggedInUser();
        Car car = carRepository.findById(carId).orElseThrow(
                ()-> new ResourceNotFoundException("Car not found with id: " + carId)
        );
        if (!likeRepository.existsByCarIdAndUserId(carId,user.getId())){
            throw new DuplicateLikeException("Car already unliked");
        }
        Like like = likeRepository.findByCarAndUser(car,user);
        likeRepository.delete(like);
    }

    @Override
    public Set<CarDTO> getUserLikedCars() {
        User user = retriveLoggedInUser();
        Set<CarDTO> carDTOS = likeRepository.findLikedCarsByUserId(user.getId());
        return carDTOS;
    }


    @Override
    public Boolean hasUserLikedCar(Long carId) {
        User user = retriveLoggedInUser();
        Car car = carRepository.findById(carId).orElseThrow(
                ()-> new ResourceNotFoundException("Car not found with id: " + carId)
        );
        if (likeRepository.existsByCarAndUser(car,user)){
            return true;
        }
        return false;
    }

    private User retriveLoggedInUser() {
        Long userId = currentAuthenticatedUserService.requireCurrentUserIdAsLong();
        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            throw new UsernameNotFoundException("User Not Found");
        }
        return user.get();
    }
}
