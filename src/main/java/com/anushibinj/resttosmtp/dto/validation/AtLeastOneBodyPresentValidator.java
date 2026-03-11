package com.anushibinj.resttosmtp.dto.validation;

import com.anushibinj.resttosmtp.dto.EmailProxyRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

/**
 * Validates that at least one of {@code text} or {@code html} is non-blank on the
 * {@link EmailProxyRequest}.
 */
public class AtLeastOneBodyPresentValidator
        implements ConstraintValidator<AtLeastOneBodyPresent, EmailProxyRequest> {

    @Override
    public boolean isValid(EmailProxyRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true; // null-object is handled by other constraints
        }
        return StringUtils.hasText(request.getText()) || StringUtils.hasText(request.getHtml());
    }
}
