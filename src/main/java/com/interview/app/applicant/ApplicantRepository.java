package com.interview.app.applicant;

import org.springframework.data.jpa.repository.JpaRepository;

/** REPOSITORY: the only code that talks to the {@code applicants} table. Spring Data generates the implementation. */
public interface ApplicantRepository extends JpaRepository<Applicant, Long> {
}
