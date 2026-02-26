package za.ac.skiet.vyou.service.Content;

import org.springframework.stereotype.Service;
import za.ac.skiet.vyou.domain.Content;
import za.ac.skiet.vyou.service.IService;

import java.util.List;

public interface IContentService extends IService<Content, java.util.UUID> {
    List<Content> getAll();
}

