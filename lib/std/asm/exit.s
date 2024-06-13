.global _exit
_exit:
	ld a0, 8(sp)	# int status
	li a7, 93	# 93 is the linux system call number for exit
	ecall		# System call
