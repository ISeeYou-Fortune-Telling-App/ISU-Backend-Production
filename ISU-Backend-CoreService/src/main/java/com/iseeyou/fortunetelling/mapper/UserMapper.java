package com.iseeyou.fortunetelling.mapper;

import com.iseeyou.fortunetelling.dto.response.account.SimpleSeerCardResponse;
import com.iseeyou.fortunetelling.dto.response.user.CustomerProfileResponse;
import com.iseeyou.fortunetelling.dto.response.user.SeerProfileResponse;
import com.iseeyou.fortunetelling.dto.response.user.UserResponse;
import com.iseeyou.fortunetelling.entity.user.SeerProfile;
import com.iseeyou.fortunetelling.entity.user.User;
import com.iseeyou.fortunetelling.util.Constants;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserMapper extends BaseMapper {

    @Autowired
    public UserMapper(ModelMapper modelMapper) {
        super(modelMapper);
    }

    @Override
    protected void configureCustomMappings() {
        modelMapper.typeMap(User.class, UserResponse.class)
                .addMappings(mapper -> {
                    mapper.using(ctx -> {
                        User user = (User) ctx.getSource();

                        if (user.getRole() == Constants.RoleEnum.CUSTOMER && user.getCustomerProfile() != null) {
                            return modelMapper.map(user.getCustomerProfile(), CustomerProfileResponse.class);
                        } else if (user.getRole() == Constants.RoleEnum.SEER && user.getSeerProfile() != null) {
                            SeerProfileResponse seerProfileResponse = modelMapper.map(user.getSeerProfile(), SeerProfileResponse.class);
                            // Handle null values with defaults
                            Constants.SeerTier seerTier = user.getSeerProfile().getSeerTier();
                            seerProfileResponse.setSeerTier(seerTier != null ? seerTier.getValue() : Constants.SeerTier.APPRENTICE.getValue());
                            if (seerProfileResponse.getTotalRates() == null) {
                                seerProfileResponse.setTotalRates(0);
                            }
                            if (seerProfileResponse.getAvgRating() == null) {
                                seerProfileResponse.setAvgRating(0.0);
                            }
                            return seerProfileResponse;
                        }

                        return null;
                    }).map(src -> src, (dest, value) -> dest.setProfile(value));
                });

        // Mapping for User to SimpleSeerCardResponse
        modelMapper.typeMap(User.class, SimpleSeerCardResponse.class)
                .addMappings(mapper -> {
                    mapper.map(User::getId, SimpleSeerCardResponse::setId);
                    mapper.map(User::getFullName, SimpleSeerCardResponse::setName);
                    mapper.map(User::getAvatarUrl, SimpleSeerCardResponse::setAvatarUrl);
                    mapper.map(User::getProfileDescription, SimpleSeerCardResponse::setProfileDescription);

                    // Handle null values with defaults
                    mapper.using(ctx -> {
                        User user = (User) ctx.getSource();
                        SeerProfile seerProfile = user.getSeerProfile();
                        if (seerProfile == null) {
                            return 0.0;
                        }
                        Double avgRating = seerProfile.getAvgRating();
                        return avgRating != null ? avgRating : 0.0;
                    }).map(src -> src, SimpleSeerCardResponse::setRating);

                    mapper.using(ctx -> {
                        User user = (User) ctx.getSource();
                        SeerProfile seerProfile = user.getSeerProfile();
                        if (seerProfile == null) {
                            return 0;
                        }
                        Integer totalRates = seerProfile.getTotalRates();
                        return totalRates != null ? totalRates : 0;
                    }).map(src -> src, SimpleSeerCardResponse::setTotalRates);

                    // Custom mapping for specialities
                    mapper.using(ctx -> {
                        User user = (User) ctx.getSource();
                        return user.getSeerSpecialities().stream()
                                .map(ss -> ss.getKnowledgeCategory().getName())
                                .toList();
                    }).map(src -> src, SimpleSeerCardResponse::setSpecialities);
                });
    }
}
