package ru.ms.second.team.registration.util;

import org.passay.CharacterData;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class PasswordManager {
    @Value("${password.length}")
    private int passwordLength;

    public String createPassword() {

        CharacterRule specialCharacterRule = new CharacterRule(new CharacterData() {
            @Override
            public String getErrorCode() {
                return "Error occurred while generating password special char";
            }

            @Override
            public String getCharacters() {
                return "!@#$%^&*()-+_";
            }
        });

        List<CharacterRule> rules = Arrays.asList(
                new CharacterRule(EnglishCharacterData.LowerCase),
                new CharacterRule(EnglishCharacterData.Digit),
                new CharacterRule(EnglishCharacterData.UpperCase),
                specialCharacterRule
        );
        PasswordGenerator passwordGenerator = new PasswordGenerator();
        return passwordGenerator.generatePassword(passwordLength, rules);
    }
}