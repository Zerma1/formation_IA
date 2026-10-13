package com.example.iaspringlab.web;

import com.example.iaspringlab.exception.InactiveProductException;
import com.example.iaspringlab.exception.InvalidOrderStatusTransitionException;
import com.example.iaspringlab.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

/**
 * Turns business errors raised from the web interface into a flash message displayed on the previous page.
 */
@ControllerAdvice(basePackages = "com.example.iaspringlab.web")
public class WebExceptionHandler {

    @ExceptionHandler({
            ResourceNotFoundException.class,
            InactiveProductException.class,
            InvalidOrderStatusTransitionException.class,
            IllegalArgumentException.class
    })
    public String handleBusinessError(RuntimeException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        return redirectBackWithError(ex.getMessage(), request, redirectAttributes);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolation(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        return redirectBackWithError(
                "Opération impossible : l'élément est encore utilisé (par exemple un produit présent dans des commandes).",
                request, redirectAttributes);
    }

    private String redirectBackWithError(String message, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error", message);
        return "redirect:" + sameSiteReferer(request);
    }

    /**
     * Path of the referring page, restricted to this application to avoid open redirects.
     */
    private String sameSiteReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null) {
            return "/";
        }
        try {
            URI uri = URI.create(referer);
            if (uri.getPath() == null || !uri.getPath().startsWith("/")) {
                return "/";
            }
            return uri.getRawQuery() == null ? uri.getRawPath() : uri.getRawPath() + "?" + uri.getRawQuery();
        } catch (IllegalArgumentException e) {
            return "/";
        }
    }
}
