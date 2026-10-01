package com.jobportal.controller.page;

import com.jobportal.exception.BadRequestException;
import com.jobportal.exception.ResourceNotFoundException;
import com.jobportal.exception.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

/**
 * Renders friendly error pages for exceptions thrown by the server-rendered (Thymeleaf) controllers.
 * REST controllers are handled separately by {@link com.jobportal.exception.GlobalExceptionHandler}.
 */
@ControllerAdvice(basePackages = "com.jobportal.controller.page")
public class PageExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(PageExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return errorView("error/404", HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ModelAndView handleForbidden(UnauthorizedException ex, HttpServletRequest request) {
        return errorView("error/403", HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ModelAndView handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return errorView("error/error", HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    private ModelAndView errorView(String view, HttpStatus status, String message, HttpServletRequest request) {
        log.warn("Page error {} at {}: {}", status.value(), request.getRequestURI(), message);
        ModelAndView mav = new ModelAndView(view);
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("error", status.getReasonPhrase());
        mav.addObject("message", message);
        return mav;
    }
}
