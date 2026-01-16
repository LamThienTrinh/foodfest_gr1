package com.foodfest.app.features.tag

class TagService(
	private val tagRepository: TagRepository
) {
	suspend fun getTags(type: String?): List<Tag> {
		val normalized = type?.uppercase()?.takeIf { it.isNotBlank() }
		return tagRepository.getAll(normalized)
	}

	suspend fun getTagIdsByNames(type: String, names: List<String>): List<Int> {
		if (names.isEmpty()) return emptyList()
		return tagRepository.findIdsByNames(type, names)
	}
}
