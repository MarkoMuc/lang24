# putcstr(SL, *char, len)
.section .text
.global _putcstr

_putcstr:
	
	sd fp, -16(sp)	# Save old FP
	mv fp, sp	# New FP points to the start of this frame
	sd a0, -24(fp)	# Save used regs
	sd a1, -32(fp)
	sd a2, -40(fp)
	sd a6, -48(fp)
	sd a7, -56(fp)

	addi sp, fp, -56 # New stack pointer
	
	li a0, 1	# fd = 1
	ld a6, 8(fp)	# get addrs
	mv a1, a6	# buff = sp address
	ld a6, 16(fp)	# get len
	mv a2, a6	# len
	li a7, 64	# 64 is the linux system call number for write
	ecall		# calls write()
	
	sd a0, 0(fp)	# Return value
	
	mv sp, fp	# Restore old SP
	ld fp, -16(sp)	# Load old FP
	ld a0, -24(sp)	# Load used regs
	ld a1, -32(sp)
	ld a2, -40(sp)
	ld a6, -48(sp)
	ld a7, -56(sp)
	ret
