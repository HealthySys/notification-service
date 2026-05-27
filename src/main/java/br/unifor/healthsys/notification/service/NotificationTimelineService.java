package br.unifor.healthsys.notification.service;

import br.unifor.healthsys.notification.model.Notification;
import br.unifor.healthsys.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationTimelineService {

    private final NotificationRepository repository;

    public NotificationTimelineService(NotificationRepository repository) {
        this.repository = repository;
    }

    public Optional<Notification> register(Notification notification) {
        if (notification.getId() == null || notification.getId().isBlank()) {
            notification.setId(UUID.randomUUID().toString());
        }
        if (notification.getTimestamp() == null) {
            notification.setTimestamp(LocalDateTime.now());
        }
        if (repository.existsById(notification.getId())) {
            return Optional.empty();
        }
        return Optional.of(repository.save(notification));
    }

    public Page<Notification> findAll(int page, int size) {
        return repository.findAllByOrderByTimestampDesc(PageRequest.of(page, size));
    }

    public Page<Notification> findForRole(String role, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        if (role == null || role.isBlank()) {
            return repository.findAllByOrderByTimestampDesc(pageable);
        }
        return repository.findVisibleForRole(role, pageable);
    }

}
