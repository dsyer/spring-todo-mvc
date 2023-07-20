<ul id="todos" class="todo-list" <#if action?has_content>hx-swap-oob="${action}"</#if>>
	<!-- These are here just to show the structure of the list items -->
	<!-- List items should get the class `editing` when editing and `completed` when marked as completed -->
	<#list todos as todo><#include "todo.ftlh"></#list>
</ul>

