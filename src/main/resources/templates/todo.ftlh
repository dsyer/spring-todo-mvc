<li <#if action?has_content>hx-swap-oob="${action}" </#if>id="todo-${todo.id()}"<#if todo.completed()> class="completed"</#if>>
	<div class="view">
		<form method="put" action="/${todo.id()}/toggle?filter=${filter!}" hx-swap="none">
			<input type="hidden" name="_method" value="put"/>
			<input class="toggle" type="checkbox"<#if todo.completed()> checked=</#if> onchange="this.form.requestSubmit()"></index>
			<label>${todo.title()}</label>
		</form>
		
		<form method="delete" action="/${todo.id()}?filter=${filter!}" hx-swap="none">
			<input type="hidden" name="_method" value="delete"/>
			<button class="destroy" type="submit"></button>
		</form>
	</div>
	<input class="edit" value="Create a TodoMVC template"></input>
</li>
