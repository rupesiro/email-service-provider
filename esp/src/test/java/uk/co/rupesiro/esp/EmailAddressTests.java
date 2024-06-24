package uk.co.rupesiro.esp;

import net.jqwik.api.*;
import net.jqwik.web.api.Web;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Label("EmailAddress Unit Tests")
public class EmailAddressTests {
    @Property(tries = 2_500)
    @Label("Valid email addresses")
    void valid(@ForAll("validEmailAddresses") String rawEmailAddress) {
        EmailAddress emailAddress = EmailAddress.from(rawEmailAddress);
        assertEquals(rawEmailAddress, emailAddress.toString());
    }

    @Property(tries = 10_000)
    @Label("Invalid email addresses")
    void invalid(@ForAll("invalidEmailAddresses") String rawEmailAddress) {
        assertThrows(IllegalArgumentException.class, () -> EmailAddress.from(rawEmailAddress));
    }

    @Provide
    Arbitrary<String> validEmailAddresses() {
        // We can't use Web.emails() since we are basing our email validation on the stricter OWASP validation,
        // therefore we need to manually set up the randomization (based on the DefaultEmailArbitrary code).
        Arbitrary<String> arbitraryLocalPart = localPart();
        Arbitrary<String> arbitraryHost = host();
        return Combinators.combine(arbitraryLocalPart, arbitraryHost).as((localPart, host) -> localPart + '@' + host);
    }

    @Provide
    Arbitrary<String> invalidEmailAddresses() {
        return Arbitraries.frequencyOf(
                Tuple.of(1, emailAddressesWithIllegalCharacters()),
                Tuple.of(1, noAtSign()),
                Tuple.of(1, ipv4EmailAddress()),
                Tuple.of(1, ipv6EmailAddress()),
                Tuple.of(1, quotedLocalPartEmailAddress())
        ).injectNull(0.01);
    }

    private Arbitrary<String> quotedLocalPartEmailAddress() {
        return Combinators.combine(quotedLocalPart(), host())
                .as((localPart, host) -> localPart + '@' + host);
    }

    private Arbitrary<String> ipv4EmailAddress() {
        return Combinators.combine(localPart(), hostIpv4())
                .as((localPart, host) -> localPart + '@' + host);
    }

    private Arbitrary<String> ipv6EmailAddress() {
        return Combinators.combine(localPart(), hostIpv6())
                .as((localPart, host) -> localPart + '@' + host);
    }

    private Arbitrary<String> localPart() {
        return Arbitraries.strings()
                .withChars("abcdefghijklmnopqrstuvwxyz")
                .withChars("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
                .withChars("0123456789&*+-_.")
                .ofMinLength(1)
                .filter(it -> !it.contains("..") && !it.startsWith(".") && !it.endsWith("."))
                .edgeCases(it -> it.includeOnly("A", "a", "0"));
    }

    private Arbitrary<String> quotedLocalPart() {
        return Arbitraries.strings()
                .withChars("abcdefghijklmnopqrstuvwxyz")
                .withChars("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
                .withChars("0123456789&*+-_.")
                .ofMinLength(1)
                .filter(it -> !it.contains("..") && !it.startsWith(".") && !it.endsWith("."))
                .map(it -> '"' + it + '"')
                .edgeCases(it -> it.includeOnly("\"a\"", "\" \""));
    }

    private Arbitrary<String> host() {
        return Web.webDomains()
                .filter(it -> {
                    String[] sections = it.split("\\.");
                    return sections[sections.length - 1].matches("^[a-zA-Z]+$");
                });
    }

    private Arbitrary<String> emailAddressesWithIllegalCharacters() {
        Arbitrary<String> illegalCharacters = Arbitraries.strings()
                .excludeChars("abcdefghijklmnopqrstuvwxyz".toCharArray())
                .excludeChars("ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray())
                .excludeChars("0123456789&*+-_.".toCharArray())
                .ofLength(1);
        Arbitrary<Integer> insertPosition = Arbitraries.integers();
        Arbitrary<String> validEmailAddresses = validEmailAddresses();
        return Combinators.combine(illegalCharacters, insertPosition, validEmailAddresses)
                .as((character, index, address) -> {
                    String[] sections = address.split("@");
                    int sectionIndex;
                    if (index > 0) {
                        sectionIndex = 0;
                    } else {
                        sectionIndex = 1;
                        index *= -1;
                    }
                    index = index % sections[sectionIndex].length();
                    String left = sections[sectionIndex].substring(0, index);
                    String right = sections[sectionIndex].substring(index);
                    sections[sectionIndex] = left + character + right;
                    return sections[0] + '@' + sections[1];
                });
    }

    private Arbitrary<String> noAtSign() {
        Arbitrary<String> arbitraryLocalPart = localPart();
        Arbitrary<String> arbitraryHost = host();
        return Combinators.combine(arbitraryLocalPart, arbitraryHost).as((localPart, host) -> localPart + host);
    }

    // Copied from DefaultEmailArbitrary

    private Arbitrary<String> hostIpv4() {
        Arbitrary<Integer> addressPart = Arbitraries.integers().between(0, 255).edgeCases(c -> c.includeOnly(0, 255));
        return Combinators.combine(addressPart, addressPart, addressPart, addressPart)
                .as((a, b, c, d) -> "[" + a + "." + b + "." + c + "." + d + "]")
                .edgeCases(stringConfig -> stringConfig.includeOnly("[0.0.0.0]", "[255.255.255.255]").add("[127.0.0.1]"));
    }

    private Arbitrary<String> hostIpv6() {
        Arbitrary<List<String>> addressParts = ipv6Part().list().ofSize(8);
        Arbitrary<String> plainAddress = addressParts.map(parts -> String.join(":", parts));
        return plainAddress
                .map(this::removeThreeOrMoreColons)
                .filter(this::validUseOfColonInIPv6Address)
                .map(plain -> "[" + plain + "]")
                .edgeCases(stringConfig -> stringConfig.includeOnly(
                        "[::]",
                        "[0:0:0:0:0:0:0:0]",
                        "[ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff]",
                        "[FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF]"
                ));
    }

    private Arbitrary<String> ipv6Part() {
        Arbitrary<Integer> ipv6PartNumber = Arbitraries.integers().between(0, 0xffff);
        return Arbitraries.frequencyOf(
                Tuple.of(1, Arbitraries.just("")),
                Tuple.of(8, ipv6PartNumber.map(this::toLowerHex)),
                Tuple.of(1, ipv6PartNumber.map(this::toUpperHex))
        );
    }

    private String removeThreeOrMoreColons(String address) {
        while (address.contains(":::")) {
            address = address.replace(":::", "::");
        }
        return address;
    }

    private boolean validUseOfColonInIPv6Address(String ip) {
        if (hasSingleColonAtStartOrEnd(ip)) {
            return false;
        }
        return !notOnlyFirstColonClusterHasDoubleColon(ip);
    }

    private static boolean notOnlyFirstColonClusterHasDoubleColon(String ip) {
        boolean first = true;
        boolean inCheck = false;
        for (int i = 0; i < ip.length() - 1; i++) {
            boolean ipContainsTwoColonsAtI = ip.charAt(i) == ':' && (ip.charAt(i + 1) == ':');
            if (first) {
                if (ipContainsTwoColonsAtI) {
                    first = false;
                    inCheck = true;
                }
            } else if (ipContainsTwoColonsAtI && !inCheck) {
                return true;
            } else if (!ipContainsTwoColonsAtI) {
                inCheck = false;
            }
        }
        return false;
    }

    private static boolean hasSingleColonAtStartOrEnd(String ip) {
        boolean startsWithOnlyOneColon = ip.charAt(0) == ':' && ip.charAt(1) != ':';
        boolean endsWithOnlyOneColon = ip.charAt(ip.length() - 1) == ':' && ip.charAt(ip.length() - 2) != ':';
        return startsWithOnlyOneColon || endsWithOnlyOneColon;
    }

    private String toLowerHex(int ipv6Part) {
        return Integer.toHexString(ipv6Part);
    }

    private String toUpperHex(int ipv6Part) {
        return toLowerHex(ipv6Part).toUpperCase();
    }
}
