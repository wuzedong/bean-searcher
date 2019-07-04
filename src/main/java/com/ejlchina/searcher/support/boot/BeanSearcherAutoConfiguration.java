package com.ejlchina.searcher.support.boot;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ejlchina.searcher.SearchParamResolver;
import com.ejlchina.searcher.SearchResultResolver;
import com.ejlchina.searcher.SearchSqlExecutor;
import com.ejlchina.searcher.SearchSqlResolver;
import com.ejlchina.searcher.Searcher;
import com.ejlchina.searcher.SearcherException;
import com.ejlchina.searcher.dialect.Dialect;
import com.ejlchina.searcher.dialect.MySqlDialect;
import com.ejlchina.searcher.dialect.OracleDialect;
import com.ejlchina.searcher.dialect.PostgreSqlDialect;
import com.ejlchina.searcher.dialect.SqlServerDialect;
import com.ejlchina.searcher.implement.MainSearchParamResolver;
import com.ejlchina.searcher.implement.MainSearchResultResolver;
import com.ejlchina.searcher.implement.MainSearchSqlExecutor;
import com.ejlchina.searcher.implement.MainSearchSqlResolver;
import com.ejlchina.searcher.implement.convertor.DefaultFieldConvertor;
import com.ejlchina.searcher.implement.convertor.FieldConvertor;
import com.ejlchina.searcher.implement.pagination.MaxOffsetPagination;
import com.ejlchina.searcher.implement.pagination.PageNumPagination;
import com.ejlchina.searcher.implement.pagination.Pagination;
import com.ejlchina.searcher.implement.parafilter.ParamFilter;
import com.ejlchina.searcher.support.boot.BeanSearcherProperties.FieldConvertorProps;
import com.ejlchina.searcher.support.boot.BeanSearcherProperties.PaginationPorps;
import com.ejlchina.searcher.support.boot.BeanSearcherProperties.ParamsPorps;
import com.ejlchina.searcher.support.boot.BeanSearcherProperties.SqlProps;
import com.ejlchina.searcher.support.spring.SpringSearcher;



@Configuration
@EnableConfigurationProperties(BeanSearcherProperties.class)
public class BeanSearcherAutoConfiguration {

	
	
	@Bean
	public Pagination pagination(BeanSearcherProperties config) {
		PaginationPorps conf = config.getParams().getPagination();
		String type = conf.getType();
		if (PaginationPorps.TYPE_PAGE.equals(type)) {
			PageNumPagination p = new PageNumPagination();
			p.setMaxAllowedSize(conf.getMaxAllowedSize());
			p.setMaxParamName(conf.getSize());
			p.setPageParamName(conf.getPage());
			p.setStartPage(conf.getStart());
			return p;
		} 
		if (PaginationPorps.TYPE_OFFSET.equals(type)) {
			MaxOffsetPagination p = new MaxOffsetPagination();
			p.setMaxAllowedSize(conf.getMaxAllowedSize());
			p.setMaxParamName(conf.getMax());
			p.setOffsetParamName(conf.getOffset());
			p.setStartOffset(conf.getStart());
			return p;
		}
		throw new SearcherException("配置项【spring.bean-searcher.params.pagination.type】只能为 page 或  offset！");
	}
	
	
	@Bean
	public SearchParamResolver searchParamResolver(Pagination pagination, BeanSearcherProperties config, 
			ObjectProvider<ParamFilter[]> paramFilterProvider) {
		MainSearchParamResolver searchParamResolver = new MainSearchParamResolver();
		searchParamResolver.setPagination(pagination);
		ParamsPorps conf = config.getParams();
		searchParamResolver.setDefaultMax(conf.getPagination().getDefaultSize());
		searchParamResolver.setFilterOperationParamNameSuffix(conf.getOperatorKey());
		searchParamResolver.setIgnoreCaseParamNameSuffix(conf.getIgnoreCaseKey());
		searchParamResolver.setOrderParamName(conf.getOrder());
		searchParamResolver.setSortParamName(conf.getSort());
		searchParamResolver.setParamNameSeparator(conf.getSeparator());
		ParamFilter[] paramFilters = paramFilterProvider.getIfAvailable();
		if (paramFilters != null) {
			searchParamResolver.setParamFilters(paramFilters);
		}
		return searchParamResolver;
	}
	
	
	@Bean
	public Dialect dialect(BeanSearcherProperties config) {
		switch (config.getSql().getDialect()) {
		case SqlProps.DIALECT_MYSQL:
			return new MySqlDialect();
		case SqlProps.DIALECT_ORACLE:
			return new OracleDialect();
		case SqlProps.DIALECT_POSTGRE_SQL:
			return new PostgreSqlDialect();
		case SqlProps.DIALECT_SQL_SERVER:
			return new SqlServerDialect();
		}
		throw new SearcherException("配置项【spring.bean-searcher.sql.dialect】只能为  MySql|Oracle|PostgreSql|SqlServer 中的一个 ！");
	}
	
	
	@Bean
	public SearchSqlResolver searchSqlResolver(Dialect dialect) {
		return new MainSearchSqlResolver(dialect);
	}
	
	@Bean
	public SearchSqlExecutor searchSqlExecutor(DataSource dataSource) {
		return new MainSearchSqlExecutor(dataSource);
	}
	
	@Bean
	public FieldConvertor fieldConvertor(BeanSearcherProperties config) {
		DefaultFieldConvertor convertor = new DefaultFieldConvertor();
		FieldConvertorProps conf = config.getFieldConvertor();
		convertor.setTrues(conf.getTrues());
		convertor.setFalses(conf.getFalses());
		return convertor;
	}
	
	
	@Bean
	public SearchResultResolver searchResultResolver(FieldConvertor fieldConvertor) {
		return new MainSearchResultResolver(fieldConvertor);
	}
	
	
	@Bean
	public Searcher beanSearcher(SearchParamResolver searchParamResolver, 
				SearchSqlResolver searchSqlResolver, 
				SearchSqlExecutor searchSqlExecutor, 
				SearchResultResolver searchResultResolver,
				BeanSearcherProperties config) {
		 SpringSearcher searcher = new SpringSearcher();
		 searcher.setScanPackages(config.getPackages());
		 searcher.setPrifexSeparatorLength(config.getPrifexSeparatorLength());
		 searcher.setSearchParamResolver(searchParamResolver);
		 searcher.setSearchSqlResolver(searchSqlResolver);
		 searcher.setSearchSqlExecutor(searchSqlExecutor);
		 searcher.setSearchResultResolver(searchResultResolver);
		 return searcher;
	}
	
	
}