package com.syfe.finance.service;
import com.syfe.finance.repository.UserRepository; import org.springframework.security.core.userdetails.*; import org.springframework.stereotype.Service;
@Service public class CustomUserDetailsService implements UserDetailsService {
 private final UserRepository users; public CustomUserDetailsService(UserRepository users){this.users=users;}
 @Override public UserDetails loadUserByUsername(String username){var u=users.findByUsernameIgnoreCase(username).orElseThrow(()->new UsernameNotFoundException("Invalid username or password")); return User.withUsername(u.getUsername()).password(u.getPasswordHash()).roles("USER").build();}
}
