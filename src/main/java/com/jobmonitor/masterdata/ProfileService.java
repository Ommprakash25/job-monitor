package com.jobmonitor.masterdata;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProfileService {

    private final ProfileRepository profiles;

    public ProfileService(ProfileRepository profiles) {
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public Profile get() {
        return profiles.findById(Profile.SINGLE_ID)
                .orElseThrow(() -> new IllegalStateException("The profile row is missing. Did the database setup finish?"));
    }

    public void save(ProfileForm form) {
        Profile profile = get();
        profile.setFullName(strip(form.getFullName()));
        profile.setPhone(strip(form.getPhone()));
        profile.setSignature(strip(form.getSignature()));
        profile.setUpdatedAt(Instant.now());
    }

    private static String strip(String value) {
        return value == null ? "" : value.strip();
    }
}
