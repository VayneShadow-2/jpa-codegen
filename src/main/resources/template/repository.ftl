package ${packageName};

import ${entity.fullName};
import org.springframework.data.jpa.repository.JpaRepository;
<#if imports??>
<#list imports as import>
import ${import};
</#list>
</#if>

/**
 * ${comments}
 *
 * @author ${author}
 * Created On ${date}.
 */
public interface ${className} extends JpaRepository<${entity.simpleName}, ${entity.idClass.simpleName}> {
}