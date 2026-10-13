package com.syfe.finance.config;
import com.syfe.finance.entity.*; import com.syfe.finance.repository.CategoryRepository; import org.springframework.boot.CommandLineRunner; import org.springframework.context.annotation.Bean; import org.springframework.context.annotation.Configuration;
@Configuration public class DefaultCategorySeeder {
 @Bean CommandLineRunner seedCategories(CategoryRepository repo){return args->{seed(repo,"Salary",CategoryType.INCOME);seed(repo,"Food",CategoryType.EXPENSE);seed(repo,"Rent",CategoryType.EXPENSE);seed(repo,"Transportation",CategoryType.EXPENSE);seed(repo,"Entertainment",CategoryType.EXPENSE);seed(repo,"Healthcare",CategoryType.EXPENSE);seed(repo,"Utilities",CategoryType.EXPENSE);};}
 private void seed(CategoryRepository r,String name,CategoryType type){if(r.findByNameIgnoreCaseAndOwnerIsNull(name).isEmpty())r.save(new Category(name,type,null,false));}
}
