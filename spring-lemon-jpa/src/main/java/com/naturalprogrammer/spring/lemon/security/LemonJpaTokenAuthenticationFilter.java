package com.naturalprogrammer.spring.lemon.security;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.naturalprogrammer.spring.lemon.commons.security.JwtService;
import com.naturalprogrammer.spring.lemon.commons.security.UserDto;
import com.naturalprogrammer.spring.lemon.commonsweb.security.LemonCommonsWebTokenAuthenticationFilter;
import com.naturalprogrammer.spring.lemon.domain.AbstractUser;
import com.naturalprogrammer.spring.lemon.util.LemonUtils;
import com.nimbusds.jwt.JWTClaimsSet;

public class LemonJpaTokenAuthenticationFilter<U extends AbstractUser<ID>, ID extends Serializable>
	extends LemonCommonsWebTokenAuthenticationFilter {

    private static final Log log = LogFactory.getLog(LemonJpaTokenAuthenticationFilter.class);

    private LemonUserDetailsService<U, ID> userDetailsService;
	
	public LemonJpaTokenAuthenticationFilter(JwtService jwtService,
			LemonUserDetailsService<U, ID> userDetailsService) {
		
		super(jwtService);
		this.userDetailsService = userDetailsService;
		
		log.info("Created");		
	}

	@Override
	protected UserDto fetchUserDto(JWTClaimsSet claims) {
		
        String username = claims.getSubject();
        U user = userDetailsService.findUserByUsername(username)
        		.orElseThrow(() -> new UsernameNotFoundException(username));

        log.debug("User found ...");

        LemonUtils.ensureCredentialsUpToDate(claims, user);
        UserDto userDto = user.toUserDto();
        userDto.setPassword(null);
        
        return userDto;
	}
}
