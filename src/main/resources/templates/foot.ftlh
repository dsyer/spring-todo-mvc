<!-- This footer should be hidden by default and shown when there are todos -->
<footer id="foot" class="footer" <#if numberOfTodos gt 0>hx-swap-oob="true"</#if>>
	<!-- This should be `0 items left` by default -->
	<span class="todo-count"><strong>${numberOfIncomplete}</strong> todo(s) left</span>
	<!-- Remove this if you don't implement routing -->
	<ul class="filters">
		<li>
			<a <#if filter??>class="selected"</#if> href="?" hx-swap="none">All</a>
		</li>
		<li>
			<a <#if filter!  == 'active'>class="selected"</#if> href="?filter=active" hx-swap="none">Active</a>
		</li>
		<li>
			<a <#if filter!  == 'completed'>class="selected"</#if> href="?filter=completed" hx-swap="none">Completed</a>
		</li>
	</ul>
	<!-- Hidden if no completed items are left ↓ -->
	<form method="delete" action="/completed?filter=${filter!}" hx-swap="none">
		<button class="clear-completed" type="submit">Clear completed</button>
	</form>
</footer>
