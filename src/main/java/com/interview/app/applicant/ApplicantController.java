package com.interview.app.applicant;

import com.interview.app.applicant.ApplicantDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * CONTROLLER: HTTP in/out only; every method delegates to {@link ApplicantService}.
 * <pre>
 *   POST   /api/applicants        create    201 | 400 | 422
 *   GET    /api/applicants        list      200
 *   GET    /api/applicants/{id}   get one   200 | 400 | 404
 *   PUT    /api/applicants/{id}   update    200 | 400 | 404 | 422
 *   DELETE /api/applicants/{id}   delete    204 | 400 | 404
 * </pre>
 */
@RestController
@RequestMapping("/api/applicants")
public class ApplicantController {

    private final ApplicantService service;

    public ApplicantController(ApplicantService service) {
        this.service = service;
    }

    @GetMapping
    public List<ApplicantResponse> list() {
        return service.list();
    }

    /** The applicant plus its verifications (id, status, timestamps). */
    @GetMapping("/{id}")
    public ApplicantDetailResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<ApplicantResponse> create(@Valid @RequestBody CreateApplicantRequest body) {
        ApplicantResponse created = service.create(body);
        return ResponseEntity.created(URI.create("/api/applicants/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public ApplicantResponse update(@PathVariable Long id, @Valid @RequestBody UpdateApplicantRequest body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
