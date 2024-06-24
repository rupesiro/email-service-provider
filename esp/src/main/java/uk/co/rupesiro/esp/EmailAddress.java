package uk.co.rupesiro.esp;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.regex.Pattern;

@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class EmailAddress {
    private static final Pattern EMAIL_ADDRESS_PATTERN = Pattern.compile(
            // https://owasp.org/www-community/OWASP_Validation_Regex_Repository
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}$"
    );

    String localPart;
    String host;

    @Override
    public String toString() {
        return localPart + '@' + host;
    }

    static EmailAddress from(String emailAddress) {
        if (emailAddress == null || !EMAIL_ADDRESS_PATTERN.matcher(emailAddress).find())
            throw new IllegalArgumentException("Invalid email address: " + emailAddress);
        String[] sections = emailAddress.split("@");
        return new EmailAddress(sections[0], sections[1]);
    }
}
