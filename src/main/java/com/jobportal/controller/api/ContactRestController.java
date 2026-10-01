package com.jobportal.controller.api;

import com.jobportal.dto.request.ContactRequest;
import com.jobportal.dto.response.ApiResponse;
import com.jobportal.entity.ContactMessage;
import com.jobportal.repository.ContactMessageRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/contact")
public class ContactRestController {

    private final ContactMessageRepository contactMessageRepository;

    public ContactRestController(ContactMessageRepository contactMessageRepository) {
        this.contactMessageRepository = contactMessageRepository;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submit(@Valid @RequestBody ContactRequest request) {
        ContactMessage message = new ContactMessage();
        message.setName(request.getName().trim());
        message.setEmail(request.getEmail().trim().toLowerCase());
        message.setSubject(request.getSubject().trim());
        message.setMessage(request.getMessage().trim());
        contactMessageRepository.save(message);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thanks for reaching out! Our team will reply within one business day."));
    }
}
