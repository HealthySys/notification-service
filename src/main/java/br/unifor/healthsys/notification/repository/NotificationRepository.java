package br.unifor.healthsys.notification.repository;

import br.unifor.healthsys.notification.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    Page<Notification> findAllByOrderByTimestampDesc(Pageable pageable);

    @Query("{ $or: [ { 'targetRoles': { $in: [ ?0 ] } }, { 'targetRoles': { $exists: false } }, { 'targetRoles': { $size: 0 } } ] }")
    Page<Notification> findVisibleForRole(String role, Pageable pageable);
}
