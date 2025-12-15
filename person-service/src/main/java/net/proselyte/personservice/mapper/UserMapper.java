package net.proselyte.personservice.mapper;

import lombok.Setter;
import net.proselyte.person.dto.IndividualDto;
import net.proselyte.person.dto.IndividualWriteDto;
import net.proselyte.personservice.entity.User;
import net.proselyte.personservice.util.DateTimeUtil;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.MappingConstants.ComponentModel.SPRING;
import static org.springframework.util.CollectionUtils.isEmpty;

@Mapper(
        componentModel = SPRING,
        injectionStrategy = CONSTRUCTOR
)
@Setter(onMethod_ = @Autowired)
public abstract class UserMapper {

    protected DateTimeUtil dateTimeUtil;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "created", expression = "java(dateTimeUtil.now())")
    @Mapping(target = "updated", expression = "java(dateTimeUtil.now())")
    public abstract User to(IndividualWriteDto dto);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "nickname", source = "nickname")
    @Mapping(target = "email", source = "email")
    public abstract IndividualDto from(User user);

    public List<IndividualDto> from(List<User> users) {
        return isEmpty(users)
                ? Collections.emptyList()
                : users.stream()
                .map(this::from)
                .collect(Collectors.toList());
    }

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "updated", expression = "java(dateTimeUtil.now())")
    @Mapping(target = "email", source = "email")
    @Mapping(target = "nickname", source = "nickname")
    public abstract void update(
            @MappingTarget
            User user,
            IndividualWriteDto dto
    );
}