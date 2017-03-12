package com.naturalprogrammer.spring.lemon.security;

import java.util.Map;

import javax.mail.MessagingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.PrincipalExtractor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.naturalprogrammer.spring.lemon.domain.AbstractUser;
import com.naturalprogrammer.spring.lemon.domain.AbstractUserRepository;
import com.naturalprogrammer.spring.lemon.mail.MailSender;
import com.naturalprogrammer.spring.lemon.util.LemonUtil;

@Transactional(propagation=Propagation.SUPPORTS, readOnly=true)
public abstract class AbstractPrincipalExtractor<U extends AbstractUser<U,?>>
		implements PrincipalExtractor {

    private static final Log log = LogFactory.getLog(AbstractPrincipalExtractor.class);
    
    public static final String DEFAULT = "default";

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
	private UserDetailsService userDetailsService;
    
    @Autowired
    private AbstractUserRepository<U, ?> userRepository;
    
    @Autowired
    private MailSender mailSender;
    
    protected String provider;
    protected String usernameColumnName = "email";

    @Override
    public Object extractPrincipal(Map<String, Object> map) {
    	
		try {
			
			// Return the user if it already exists
			return userDetailsService
				.loadUserByUsername((String) map.get(usernameColumnName));
			
		} catch (UsernameNotFoundException e) {
			
			return createUser(map);
		}
    }
    
	@Transactional(propagation=Propagation.REQUIRED, readOnly=false)
	protected U createUser(Map<String, Object> map) {
		
		U user = newUser(map);
		
		user.setUsername((String) map.get(usernameColumnName));
		
		String password = LemonUtil.uid();
		user.setPassword(passwordEncoder.encode(password));
		
		userRepository.save(user);
		
		LemonUtil.afterCommit(() -> {
			
			try {
				
				mailSender.send(user.getEmail(), "Your new passsword", password);
				
			} catch (MessagingException e) {
				
				log.warn("Could not send mail after registering " + user.getEmail(), e);
			}
		});
		
		return user;
	}
	
	protected abstract U newUser(Map<String, Object> principalMap);

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}  
}