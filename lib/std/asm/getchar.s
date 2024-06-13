# getchar(SL)
.equ STDIN, 0
.equ COUNT,1
.equ SYS_READ, 63

.section .text
.align 2
.global _getchar

_getchar:
	sd fp, 8(sp)	# Save old FP
	mv fp, sp	# New FP points to the start of this frame
	sd a0, 16(fp)	# Save used regs
	sd a1, 24(fp)
	sd a2, 32(fp)
	sd a6, 40(fp)
	sd a7, 48(fp)

	addi sp, fp, 56 # New stack pointer
	
	sd zero, 0(sp)	# Save 0 to the Stack
	mv a6, sp	# Save the address of the buffer
	
	li a0, STDIN	# fd = 0
	mv a1, a6	# buff = 0(sp)
	li a2, COUNT	# count = 1
	li a7, SYS_READ	# 63 is the linux system call number for read
	ecall		# calls read()
	
	ld a1, 0(sp)
	andi a1, a1, 0xFF # MOD a0 with 256 for ASCII
	sd a1, 0(fp)	# Return value
	
	mv sp, fp	# Restore old SP
	ld fp, 8(sp)	# Load old FP
	ld a0, 16(sp)	# Load used regs
	ld a1, 24(sp)
	ld a2, 32(sp)
	ld a6, 40(sp)
	ld a7, 48(sp)
	ret
