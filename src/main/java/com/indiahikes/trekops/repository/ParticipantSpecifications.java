package com.indiahikes.trekops.repository;

import com.indiahikes.trekops.domain.Participant;
import com.indiahikes.trekops.domain.ParticipantStatus;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Search filters for participants. Each method returns null when its filter is not set, and
 * Specification.where(...).and(...) skips nulls, so the filters compose freely.
 */
public final class ParticipantSpecifications {

  private static final char ESCAPE = '\\';

  private ParticipantSpecifications() {}

  public static Specification<Participant> search(
      UUID trekId, ParticipantStatus status, String name, String phone) {
    return Specification.where(hasTrek(trekId))
        .and(hasStatus(status))
        .and(nameContains(name))
        .and(phoneContains(phone));
  }

  static Specification<Participant> hasTrek(UUID trekId) {
    return trekId == null
        ? null
        : (root, query, cb) -> cb.equal(root.get("trek").get("id"), trekId);
  }

  static Specification<Participant> hasStatus(ParticipantStatus status) {
    return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
  }

  /** Case-insensitive "contains" match on the name. */
  static Specification<Participant> nameContains(String name) {
    if (name == null || name.isBlank()) {
      return null;
    }
    String pattern = "%" + escapeLike(name.trim().toLowerCase(Locale.ROOT)) + "%";
    return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, ESCAPE);
  }

  static Specification<Participant> phoneContains(String phone) {
    if (phone == null || phone.isBlank()) {
      return null;
    }
    String pattern = "%" + escapeLike(phone.trim()) + "%";
    return (root, query, cb) -> cb.like(root.get("phone"), pattern, ESCAPE);
  }

  /** Makes % and _ in user input match literally instead of acting as wildcards. */
  private static String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
