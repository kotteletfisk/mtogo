<script lang="ts">
	import favicon from '$lib/assets/favicon.svg';
	import "$lib/main.css";

	let { children } = $props();
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
	<title>Supplier</title>
</svelte:head>

<div class="header">
	<h3>Supplier header</h3>
</div>

{@render children()}
