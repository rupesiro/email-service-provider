package uk.co.rupesiro.esp;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.regex.Pattern;

/**
 * Representation of an email address according to the guidelines provided in the OWASP website.
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class EmailAddress {
    private static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
            // https://owasp.org/www-community/OWASP_Validation_Regex_Repository
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$"
    );

    /**
     * The local part (i.e. the portion before the {code @}) of an email address.
     */
    String localPart;
    /**
     * The host (i.e. the portion after the {code @}) of an email address.
     */
    String host;

    @Override
    public String toString() {
        return localPart + '@' + host;
    }

    /**
     * Constructs an email address instance from the raw email address supplied, validating it according to the OWASP
     * guidelines.
     *
     * @param emailAddress the raw email address.
     * @return the constructed email address.
     *
     * @see <a href="https://owasp.org/www-community/OWASP_Validation_Regex_Repository">OWASP Guidelines</a>
     */
    static EmailAddress from(String emailAddress) {
        if (emailAddress == null || !EMAIL_ADDRESS_PATTERN.matcher(emailAddress).find())
            throw new IllegalArgumentException("Invalid email address: " + emailAddress);
        String[] sections = emailAddress.split("@");
        return new EmailAddress(sections[0], sections[1]);
    }
}
