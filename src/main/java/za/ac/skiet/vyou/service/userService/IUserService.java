package za.ac.skiet.vyou.service.userService;

import org.springframework.stereotype.Service;
import za.ac.skiet.vyou.domain.UserEntity;
import za.ac.skiet.vyou.service.IService;

import java.util.List;
import java.util.UUID;

@Service
public interface IUserService extends IService<UserEntity, UUID> {

    List<UserEntity> getAll();

    UserEntity findByEmail(String email);
}
